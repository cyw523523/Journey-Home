package com.guitu.service;

import com.guitu.domain.Animal;
import com.guitu.domain.MedicalRecord;
import com.guitu.dto.MedicalRecordDtos;
import com.guitu.exception.BusinessException;
import com.guitu.mapper.DtoMapper;
import com.guitu.repository.AnimalRepository;
import com.guitu.repository.MedicalRecordRepository;
import com.guitu.security.SecuritySupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final AnimalRepository animalRepository;
    private final DtoMapper mapper;

    public MedicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            AnimalRepository animalRepository,
            DtoMapper mapper
    ) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.animalRepository = animalRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDtos.MedicalRecordResponse> listByAnimal(Long animalId) {
        return medicalRecordRepository.findByAnimalIdOrderByRecordDateDesc(animalId)
                .stream().map(mapper::toMedicalRecordResponse).toList();
    }

    @Transactional
    public MedicalRecordDtos.MedicalRecordResponse create(Long animalId, MedicalRecordDtos.SaveMedicalRecordRequest request) {
        Animal animal = getAnimal(animalId);
        SecuritySupport.requireOwnerOrAdmin(animal.getPublisher().getId());
        MedicalRecord record = new MedicalRecord();
        fillRecord(record, request);
        record.setAnimal(animal);
        return mapper.toMedicalRecordResponse(medicalRecordRepository.save(record));
    }

    @Transactional
    public MedicalRecordDtos.MedicalRecordResponse update(Long recordId, MedicalRecordDtos.SaveMedicalRecordRequest request) {
        MedicalRecord record = getRecord(recordId);
        SecuritySupport.requireOwnerOrAdmin(record.getAnimal().getPublisher().getId());
        fillRecord(record, request);
        return mapper.toMedicalRecordResponse(record);
    }

    @Transactional
    public void delete(Long recordId) {
        MedicalRecord record = getRecord(recordId);
        SecuritySupport.requireOwnerOrAdmin(record.getAnimal().getPublisher().getId());
        medicalRecordRepository.delete(record);
    }

    private void fillRecord(MedicalRecord record, MedicalRecordDtos.SaveMedicalRecordRequest request) {
        record.setType(request.type());
        record.setRecordDate(request.recordDate());
        record.setVeterinarianName(request.veterinarianName());
        record.setInstitution(request.institution());
        record.setDescription(request.description());
        record.getImageUrls().clear();
        if (request.imageUrls() != null) {
            record.getImageUrls().addAll(request.imageUrls());
        }
    }

    private Animal getAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "动物档案不存在"));
    }

    private MedicalRecord getRecord(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "医疗记录不存在"));
    }
}
