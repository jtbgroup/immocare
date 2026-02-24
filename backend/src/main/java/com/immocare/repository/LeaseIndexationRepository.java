package com.immocare.repository;
import com.immocare.model.entity.LeaseIndexationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface LeaseIndexationRepository extends JpaRepository<LeaseIndexationHistory, Long> {
    List<LeaseIndexationHistory> findByLeaseIdOrderByApplicationDateDesc(Long leaseId);
    @Query("SELECT COUNT(h) > 0 FROM LeaseIndexationHistory h WHERE h.lease.id = :leaseId AND YEAR(h.applicationDate) = :year")
    boolean existsByLeaseIdAndYear(@Param("leaseId") Long leaseId, @Param("year") int year);
}
