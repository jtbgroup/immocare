package com.immocare.repository;

import com.immocare.model.entity.Building;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Building entity.
 * 
 * Provides CRUD operations and custom queries for building management.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

  /**
   * Find buildings by city.
   * 
   * @param city the city name
   * @param pageable pagination information
   * @return page of buildings in the specified city
   */
  Page<Building> findByCity(String city, Pageable pageable);

  /**
   * Search buildings by name or address (case-insensitive).
   * 
   * @param searchTerm the search term
   * @param pageable pagination information
   * @return page of buildings matching the search term
   */
  @Query("SELECT b FROM Building b WHERE " +
      "LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
      "LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
      "LOWER(b.city) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  Page<Building> searchBuildings(@Param("searchTerm") String searchTerm, Pageable pageable);

  /**
   * Search buildings by city and search term.
   * 
   * @param city the city name
   * @param searchTerm the search term
   * @param pageable pagination information
   * @return page of buildings matching both filters
   */
  @Query("SELECT b FROM Building b WHERE " +
      "b.city = :city AND " +
      "(LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
      "LOWER(b.streetAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  Page<Building> searchBuildingsByCity(
      @Param("city") String city,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Find all distinct cities from buildings.
   * 
   * @return list of unique city names
   */
  @Query("SELECT DISTINCT b.city FROM Building b ORDER BY b.city")
  List<String> findDistinctCities();
}
