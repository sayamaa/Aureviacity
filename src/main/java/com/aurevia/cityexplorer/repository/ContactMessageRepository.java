package com.aurevia.cityexplorer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aurevia.cityexplorer.model.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();
}
