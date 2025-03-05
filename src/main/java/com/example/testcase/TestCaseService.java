package com.example.testcase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    // 🟢 Batch save để lưu nhiều test cases cùng lúc
    public void saveAll(List<TestCase> testCases) {
        testCaseRepository.saveAll(testCases);
    }
//    public List<TestCase> findAll() {return testCaseRepository.findAll();}

    //find hidden test cases
    public List<TestCase> findHiddenTestCases(Long id) {
        return testCaseRepository.findHiddenTestCase(id);
    }

    //find visible test cases
    public List<TestCase> findVisibleTestCases(Long id) {
        return testCaseRepository.findVisibleTestCase(id);
    }


}