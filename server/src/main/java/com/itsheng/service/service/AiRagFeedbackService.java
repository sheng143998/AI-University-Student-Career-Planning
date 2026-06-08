package com.itsheng.service.service;

import com.itsheng.pojo.dto.AiRagFeedbackDTO;
import com.itsheng.pojo.dto.AiRagSettingsDTO;
import com.itsheng.pojo.vo.AiRagFeedbackVO;
import com.itsheng.pojo.vo.AiRagSettingsVO;

public interface AiRagFeedbackService {

    AiRagFeedbackVO submitFeedback(AiRagFeedbackDTO dto);

    AiRagSettingsVO getSettings();

    AiRagSettingsVO updateSettings(AiRagSettingsDTO dto);
}
