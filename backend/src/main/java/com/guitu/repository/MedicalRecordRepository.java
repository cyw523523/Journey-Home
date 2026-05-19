package com.guitu.repository;

import com.guitu.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long>, JpaSpecificationExecutor<MedicalRecord> {

    List<MedicalRecord> findByAnimalIdOrderByRecordDateDesc(Long animalId);
}
