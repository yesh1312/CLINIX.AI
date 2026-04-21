package com.clinixai.repository;

import com.clinixai.model.AnalysisHistory;
import com.clinixai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    List<AnalysisHistory> findByUserOrderByTimestampDesc(User user);
}
