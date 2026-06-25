package com.community.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.file.domain.entity.FileAttachment;
import com.community.file.dao.mapper.FileAttachmentMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FileAttachmentDao extends ServiceImpl<FileAttachmentMapper, FileAttachment> {
}
