package com.community.message.domain.entity;

import com.community.message.domain.dto.SendMsgReq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageExtra implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> fileIds;

    private SendMsgReq.SoundMsgDTO soundMsg;
}
