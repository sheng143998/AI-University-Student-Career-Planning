package com.itsheng.service.mapper;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserVectorStoreMapperXmlTest {

    @Test
    void selectByVectorStoreIdAndUserIdFiltersBeforeReadingContent() throws Exception {
        Document document = parseMapperXml();
        Element select = findElementById(document, "select", "selectByVectorStoreIdAndUserId");

        assertNotNull(select);
        String sql = select.getTextContent().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("from ai_career_plan.user_vector_store"));
        assertTrue(sql.contains("where id = #{id}"));
        assertTrue(sql.contains("and user_id = #{userid}"));
    }

    @Test
    void contentColumnMapsToResumeContentProperty() throws Exception {
        Document document = parseMapperXml();
        Element resultMap = findElementById(document, "resultMap", "UserVectorStoreResult");

        assertNotNull(resultMap);
        NodeList results = resultMap.getElementsByTagName("result");
        boolean hasContentMapping = false;
        for (int i = 0; i < results.getLength(); i++) {
            Node node = results.item(i);
            if (node instanceof Element result
                    && "content".equals(result.getAttribute("column"))
                    && "resumeContent".equals(result.getAttribute("property"))) {
                hasContentMapping = true;
                break;
            }
        }
        assertTrue(hasContentMapping);
    }

    private Document parseMapperXml() throws Exception {
        Path mapperPath = Path.of(
                "src",
                "main",
                "resources",
                "mapper",
                "UserVectorStoreMapper.xml"
        );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(mapperPath.toFile());
    }

    private Element findElementById(Document document, String tagName, String id) {
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }
}
