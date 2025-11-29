package com.example.konect.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import com.example.konect.notice.model.Notice;

public interface NoticeRepository extends Repository<Notice, Integer> {

    Page<Notice> findAll(Pageable pageable);
}
