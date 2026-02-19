package com.immocare.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing a physical building containing housing units.
 * 
 * Business Rules:
 * - All required fields (name, streetAddress, postalCode, city, country) must be present
 * - Owner name is optional and can be inherited by housing units
 * - Building cannot be deleted if it contains housing units (enforced in service layer)
 * - Duplicate building names are allowed (different buildings in different cities)
 */
@Entity
@Table(name = "building")
public class Building {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Building name is required")
  @Size(max = 100, message = "Building name must be 100 characters or less")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @NotBlank(message = "Street address is required")
  @Size(max = 200, message = "Street address must be 200 characters or less")
  @Column(name = "street_address", nullable = false, length = 200)
  private String streetAddress;

  @NotBlank(message = "Postal code is required")
  @Size(max = 20, message = "Postal code must be 20 characters or less")
  @Column(name = "postal_code", nullable = false, length = 20)
  private String postalCode;

  @NotBlank(message = "City is required")
  @Size(max = 100, message = "City must be 100 characters or less")
  @Column(name = "city", nullable = false, length = 100)
  private String city;

  @NotBlank(message = "Country is required")
  @Size(max = 100, message = "Country must be 100 characters or less")
  @Column(name = "country", nullable = false, length = 100)
  private String country;

  @Size(max = 200, message = "Owner name must be 200 characters or less")
  @Column(name = "owner_name", length = 200)
  private String ownerName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Constructors
  public Building() {
  }

  public Building(String name, String streetAddress, String postalCode, 
                  String city, String country) {
    this.name = name;
    this.streetAddress = streetAddress;
    this.postalCode = postalCode;
    this.city = city;
    this.country = country;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStreetAddress() {
    return streetAddress;
  }

  public void setStreetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return "Building{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", city='" + city + '\'' +
        ", country='" + country + '\'' +
        '}';
  }
}
