package com.itsheng.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.itsheng.common.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 简历文件直接编辑服务。
 * 支持 docx / txt / md 格式，修改后重新上传 OSS。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeFileEditService {

    private final AliOssUtil aliOssUtil;

    /** 可直接编辑的简历文件格式（docx 因 XML 结构复杂不支持自动编辑） */
    public static final Set<String> EDITABLE_TYPES = Set.of("txt", "md");

    /** 技能区块识别关键词 */
    private static final Set<String> SKILL_SECTION_KEYWORDS =
            Set.of("专业技能", "个人技能", "技能清单", "技术栈", "掌握技能");

    /** 下一个区块的标题关键词（遇到这些则停止收集技能段落） */
    private static final Set<String> NEXT_SECTION_KEYWORDS =
            Set.of("教育经历", "工作经历", "项目经历", "实习经历", "自我评价",
                    "获奖情况", "语言能力", "自我介绍", "个人简介", "荣誉奖项");

    /** 当没有标题匹配时，回退密度算法的最小匹配数 */
    private static final int MIN_SKILL_MATCH_THRESHOLD = 2;

    // ─────────────────────────────────────────────────────────────────
    // 公共入口
    // ─────────────────────────────────────────────────────────────────

    @Nullable
    public String tryEditResumeFile(String fileUrl, String field,
                                    JsonNode oldValueNode, JsonNode newValueNode,
                                    Long userId) {
        String ext = getExtension(fileUrl);
        if (!EDITABLE_TYPES.contains(ext)) return null;

        try {
            byte[] original = aliOssUtil.download(fileUrl);
            if (original == null || original.length == 0) {
                log.warn("下载简历文件失败或文件为空: {}", fileUrl);
                return null;
            }

            byte[] modified = switch (ext) {
                case "docx" -> editDocx(original, field, oldValueNode, newValueNode);
                case "txt", "md" -> editTextFile(original, field, oldValueNode, newValueNode);
                default -> null;
            };

            if (modified == null) {
                log.info("字段 [{}] 在 {} 文件中无法自动定位修改点，跳过文件编辑", field, ext);
                return null;
            }

            String objectName = "resumes/" + userId + "/" + UUID.randomUUID() + "_edited." + ext;
            String newUrl = aliOssUtil.upload(modified, objectName);
            log.info("简历文件编辑成功 [{}] field={}: {}", ext, field, newUrl);
            return newUrl;

        } catch (Exception e) {
            log.warn("简历文件编辑失败 [{}] field={}: {}", ext, field, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 OSS URL 提取文件扩展名（小写，不含点）。
     */
    public String getExtension(String url) {
        if (url == null || url.isBlank()) return "";
        String path = url.split("[?#]")[0];
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    // ─────────────────────────────────────────────────────────────────
    // DOCX 编辑
    // ─────────────────────────────────────────────────────────────────

    @Nullable
    private byte[] editDocx(byte[] bytes, String field, JsonNode oldValueNode, JsonNode newValueNode) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            boolean replaced;

            if ("skills".equals(field) && newValueNode != null && newValueNode.isArray()) {
                replaced = replaceSkillsInDocx(doc, oldValueNode, newValueNode);
            } else if (oldValueNode != null && oldValueNode.isTextual()) {
                String oldText = oldValueNode.asText("").trim();
                String newText = newValueNode != null ? newValueNode.asText("").trim() : "";
                if (oldText.isEmpty() || newText.isEmpty() || oldText.equals(newText)) return null;
                replaced = replaceTextInDocx(doc, oldText, newText);
            } else {
                return null;
            }

            if (!replaced) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    /** DOCX 标量字段替换（直接文本匹配） */
    private boolean replaceTextInDocx(XWPFDocument doc, String oldText, String newText) {
        boolean replaced = false;
        for (XWPFParagraph para : doc.getParagraphs()) {
            if (replaceInParagraph(para, oldText, newText)) replaced = true;
        }
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        if (replaceInParagraph(para, oldText, newText)) replaced = true;
                    }
                }
            }
        }
        return replaced;
    }

    /**
     * DOCX skills 替换。
     * <p>
     * 核心逻辑：
     * 1. 找到"专业技能"标题所在段落的位置（bodyElement 的索引）。
     * 2. 收集技能区块内的所有现有段落（直到下一个区块标题）。
     * 3. 用新技能列表更新：复用现有段落（保留样式），
     *    通过 XmlCursor 在正确位置插入额外段落，
     *    清空多余的旧段落（清空而非删除，避免破坏文档结构）。
     */
    private boolean replaceSkillsInDocx(XWPFDocument doc, JsonNode oldSkillsNode, JsonNode newSkillsNode) {
        List<String> newSkills = extractTextArray(newSkillsNode);
        if (newSkills.isEmpty()) return false;

        // 收集文档中所有段落（不含表格内，先只处理主体段落）
        List<XWPFParagraph> allParas = doc.getParagraphs();

        // 找到技能区块标题
        int headerIdx = -1;
        for (int i = 0; i < allParas.size(); i++) {
            String text = allParas.get(i).getText().trim();
            if (isSkillHeader(text)) {
                headerIdx = i;
                break;
            }
        }

        if (headerIdx == -1) {
            // 没有找到标题，回退到密度算法
            return replaceSkillsByDensity(doc, oldSkillsNode, newSkills);
        }

        // 收集技能区块内的段落（不包含标题行本身，不包含空段落）
        List<XWPFParagraph> skillParas = new ArrayList<>();
        List<XWPFParagraph> emptyParasBetween = new ArrayList<>(); // 技能段落之间的空行
        for (int i = headerIdx + 1; i < allParas.size(); i++) {
            XWPFParagraph p = allParas.get(i);
            String text = p.getText().trim();
            if (isNextSectionHeader(text)) break;
            if (text.isEmpty()) {
                emptyParasBetween.add(p);
            } else {
                skillParas.add(p);
            }
        }

        if (skillParas.isEmpty()) {
            log.warn("找到技能标题但标题下方没有内容段落，放弃编辑");
            return false;
        }

        // 检测原有 bullet 前缀（如 "• "）
        String bulletPrefix = detectBulletPrefix(skillParas.get(0).getText().trim());

        int existingCount = skillParas.size();
        int newCount = newSkills.size();

        // 更新已存在的段落（在原位置，保留原样式）
        for (int i = 0; i < Math.min(existingCount, newCount); i++) {
            updateParagraphText(skillParas.get(i), bulletPrefix + newSkills.get(i));
        }

        if (newCount > existingCount) {
            // 需要在最后一个技能段落之后插入新段落
            XWPFParagraph anchorPara = skillParas.get(existingCount - 1);
            for (int i = existingCount; i < newCount; i++) {
                XWPFParagraph inserted = insertParagraphAfter(doc, anchorPara);
                updateParagraphText(inserted, bulletPrefix + newSkills.get(i));
                anchorPara = inserted;
            }
        } else if (newCount < existingCount) {
            // 多余的旧段落：清空文本（不删除，避免破坏文档 XML 结构）
            for (int i = newCount; i < existingCount; i++) {
                updateParagraphText(skillParas.get(i), "");
            }
        }

        log.info("DOCX 技能区块更新成功：原{}条 → 新{}条，bullet='{}'", existingCount, newCount, bulletPrefix);
        return true;
    }

    /** 在 anchorPara 之后插入一个新段落（使用 XmlCursor 定位，而非追加到文档末尾） */
    private XWPFParagraph insertParagraphAfter(XWPFDocument doc, XWPFParagraph anchorPara) {
        XmlCursor cursor = anchorPara.getCTP().newCursor();
        cursor.toEndToken();
        cursor.toNextToken(); // 移到 anchorPara 节点之后
        XWPFParagraph newPara = doc.insertNewParagraph(cursor);
        cursor.dispose();
        return newPara;
    }

    /** 没有标题时的回退策略：找技能最密集的段落并替换 */
    private boolean replaceSkillsByDensity(XWPFDocument doc, JsonNode oldSkillsNode, List<String> newSkills) {
        List<String> oldSkills = extractTextArray(oldSkillsNode);
        XWPFParagraph bestPara = null;
        int bestCount = 0;
        for (XWPFParagraph para : doc.getParagraphs()) {
            int count = countSkillMatches(para.getText(), oldSkills);
            if (count > bestCount) {
                bestCount = count;
                bestPara = para;
            }
        }
        if (bestPara == null || bestCount < MIN_SKILL_MATCH_THRESHOLD) return false;
        String separator = detectSeparator(bestPara.getText());
        updateParagraphText(bestPara, String.join(separator, newSkills));
        log.info("DOCX 技能段落通过密度算法更新");
        return true;
    }

    private boolean isSkillHeader(String text) {
        boolean isMarkdownHeading = text.trim().startsWith("#");
        String stripped = text.replaceAll("^#+\\s*", "")       // strip ## prefix
                              .replaceAll("[*_`~]+", "")       // strip bold/italic markers
                              .trim();
        // 非 Markdown 标题行要求内容简短（纯区块标题不超过 15 字），防止误匹配项目内"技术栈：xxx"这类长行
        if (!isMarkdownHeading && stripped.length() > 15) return false;
        return SKILL_SECTION_KEYWORDS.stream()
                .anyMatch(k -> stripped.equals(k) || stripped.startsWith(k + "\uff1a") || stripped.startsWith(k + ":"));
    }

    private boolean isNextSectionHeader(String text) {
        if (text.isEmpty()) return false;
        String stripped = text.replaceAll("^#+\\s*", "")
                              .replaceAll("[*_`~]+", "")
                              .trim();
        return NEXT_SECTION_KEYWORDS.stream().anyMatch(stripped::contains);
    }

    /** 检测段落中的 bullet 前缀，如 "• " 或 "- " 或 "* " */
    private String detectBulletPrefix(String text) {
        if (text.startsWith("• ")) return "• ";
        if (text.startsWith("● ")) return "● ";
        if (text.startsWith("◆ ")) return "◆ ";
        if (text.startsWith("- "))  return "- ";
        if (text.startsWith("* "))  return "* ";
        if (text.startsWith("• "))  return "• "; // 全角
        // 如果只有 "•" 没有空格也兼容
        if (text.startsWith("•")) return "•";
        return "";
    }

    /** 将段落中所有 Run 的文本替换为 newText，并保留第一个 Run 的格式 */
    private void updateParagraphText(XWPFParagraph para, String text) {
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) {
            para.createRun().setText(text);
        } else {
            runs.get(0).setText(text, 0);
            for (int i = runs.size() - 1; i > 0; i--) {
                para.removeRun(i);
            }
        }
    }

    private boolean replaceInParagraph(XWPFParagraph paragraph, String oldText, String newText) {
        String fullText = paragraph.getText();
        if (fullText == null || !fullText.contains(oldText)) return false;
        String replacedText = fullText.replace(oldText, newText);
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return false;
        runs.get(0).setText(replacedText, 0);
        for (int i = runs.size() - 1; i > 0; i--) {
            paragraph.removeRun(i);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    // TXT / MD 编辑
    // ─────────────────────────────────────────────────────────────────

    @Nullable
    private byte[] editTextFile(byte[] bytes, String field, JsonNode oldValueNode, JsonNode newValueNode) {
        String content = new String(bytes, StandardCharsets.UTF_8);

        if ("skills".equals(field) && newValueNode != null && newValueNode.isArray()) {
            String modified = replaceSkillsInText(content, oldValueNode, newValueNode);
            if (modified == null || modified.equals(content)) return null;
            return modified.getBytes(StandardCharsets.UTF_8);
        }

        if (oldValueNode == null || oldValueNode.isNull() || !oldValueNode.isTextual()) return null;
        String oldText = oldValueNode.asText("").trim();
        String newText = newValueNode != null ? newValueNode.asText("").trim() : "";
        if (oldText.isEmpty() || oldText.equals(newText)) return null;
        if (!content.contains(oldText)) return null;
        return content.replace(oldText, newText).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * TXT/MD skills 追加。
     * 逻辑：找到专业技能区块 → 解析现有技能行 → 只插入新增的（去重）→ 保留原有格式
     */
    @Nullable
    private String replaceSkillsInText(String content, JsonNode oldSkillsNode, JsonNode newSkillsNode) {
        List<String> newSkillsFromAI = extractTextArray(newSkillsNode);
        if (newSkillsFromAI.isEmpty()) return null;

        String[] lines = content.split("\n", -1);

        // 找技能标题行：优先匹配 Markdown # 标题，再回退到纯文本标题
        int headerIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("#") && isSkillHeader(trimmed)) {
                headerIdx = i;
                break;
            }
        }
        // 回退：没找到 Markdown 标题，再扫一遍纯文本标题
        if (headerIdx == -1) {
            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (isSkillHeader(trimmed)) {
                    headerIdx = i;
                    break;
                }
            }
        }

        if (headerIdx != -1) {
            // 跳过标题后的空行，收集现有技能行
            int contentEnd = headerIdx + 1;
            while (contentEnd < lines.length && lines[contentEnd].trim().isEmpty()) {
                contentEnd++;
            }

            List<String> existingSkillLines = new ArrayList<>();
            List<Integer> skillLineIndices = new ArrayList<>();
            while (contentEnd < lines.length) {
                String trimmed = lines[contentEnd].trim();
                if (isNextSectionHeader(trimmed)) break;
                if (!trimmed.isEmpty()) {
                    existingSkillLines.add(lines[contentEnd]);
                    skillLineIndices.add(contentEnd);
                }
                contentEnd++;
            }

            // 检测 bullet 前缀
            String bulletPrefix = existingSkillLines.isEmpty() ? "• "
                    : detectBulletPrefix(existingSkillLines.get(0).trim());

            // 提取现有技能纯名称（去掉 bullet 前缀），用于去重
            String strippedBullet = bulletPrefix.trim();
            List<String> existingNames = existingSkillLines.stream()
                    .map(l -> {
                        String t = l.trim();
                        return (!strippedBullet.isEmpty() && t.startsWith(strippedBullet))
                                ? t.substring(strippedBullet.length()).trim() : t;
                    })
                    .toList();

            // 只取 AI 传来的、文件里还没有的技能
            List<String> toAppend = newSkillsFromAI.stream()
                    .filter(s -> existingNames.stream().noneMatch(e -> e.equalsIgnoreCase(s)))
                    .map(s -> {
                        // 如果 AI 传来的描述句已经以 bullet 字符开头，不重复添加前缀
                        String trimmed = s.trim();
                        if (!bulletPrefix.isEmpty() && trimmed.startsWith(bulletPrefix.trim())) {
                            return trimmed;
                        }
                        return bulletPrefix + trimmed;
                    })
                    .toList();

            if (toAppend.isEmpty()) {
                log.info("TXT/MD 技能追加：全部已存在，无需修改");
                return null;
            }

            // 在最后一个技能行之后插入
            List<String> result = new ArrayList<>(List.of(lines));
            int insertAfter = skillLineIndices.isEmpty()
                    ? headerIdx + 1
                    : skillLineIndices.get(skillLineIndices.size() - 1) + 1;
            result.addAll(insertAfter, toAppend);
            log.info("TXT/MD 技能追加成功：新增 {} 条，保留原有 {} 条", toAppend.size(), existingSkillLines.size());
            return String.join("\n", result);
        }

        // 没有标题：回退密度算法，追加而非替换
        List<String> oldSkills = extractTextArray(oldSkillsNode);
        int matchIdx = -1;
        int bestCount = 0;
        for (int i = 0; i < lines.length; i++) {
            int count = countSkillMatches(lines[i], oldSkills);
            if (count > bestCount) {
                bestCount = count;
                matchIdx = i;
            }
        }
        if (matchIdx < 0 || bestCount < MIN_SKILL_MATCH_THRESHOLD) return null;

        String existingLine = lines[matchIdx];
        String separator = detectSeparator(existingLine);
        List<String> toAdd = newSkillsFromAI.stream()
                .filter(s -> !existingLine.contains(s))
                .toList();
        if (toAdd.isEmpty()) return null;

        lines[matchIdx] = existingLine + separator + String.join(separator, toAdd);
        log.info("TXT/MD 技能行通过密度算法追加");
        return String.join("\n", lines);
    }

    /**
     * 读取简历文件全文（仅支持 txt/md），供 AI 参考原文排版风格。
     */
    @Nullable
    public String readResumeFileRaw(String fileUrl) {
        String ext = getExtension(fileUrl);
        if (!"txt".equals(ext) && !"md".equals(ext)) return null;
        try {
            byte[] bytes = aliOssUtil.download(fileUrl);
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取简历原文失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 读取简历文件中专业技能区块的原始文本（仅支持 txt/md）。
     * 返回从标题行（含）到下一区块标题（不含）的原文，供 AI 学习排版风格。
     */
    @Nullable
    public String readSkillsSectionRaw(String fileUrl) {
        String ext = getExtension(fileUrl);
        if (!"txt".equals(ext) && !"md".equals(ext)) return null;
        try {
            byte[] bytes = aliOssUtil.download(fileUrl);
            if (bytes == null || bytes.length == 0) return null;
            String content = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\n", -1);

            int headerIdx = -1;
            for (int i = 0; i < lines.length; i++) {
                if (isSkillHeader(lines[i].trim())) { headerIdx = i; break; }
            }
            if (headerIdx == -1) return null;

            StringBuilder sb = new StringBuilder();
            for (int i = headerIdx; i < lines.length; i++) {
                if (i > headerIdx && isNextSectionHeader(lines[i].trim())) break;
                sb.append(lines[i]).append("\n");
            }
            return sb.toString().stripTrailing();
        } catch (Exception e) {
            log.warn("读取技能区块原文失败: {}", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────

    private List<String> extractTextArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        arrayNode.forEach(item -> {
            String text = item.asText("").trim();
            if (!text.isEmpty()) result.add(text);
        });
        return result;
    }

    private int countSkillMatches(String text, List<String> skills) {
        if (text == null || text.isBlank()) return 0;
        return (int) skills.stream().filter(text::contains).count();
    }

    private String detectSeparator(String text) {
        if (text.contains("、")) return "、";
        if (text.contains("，")) return "，";
        if (text.contains(", ")) return ", ";
        if (text.contains(","))  return ",";
        if (text.contains(" | ")) return " | ";
        return "、";
    }
}
