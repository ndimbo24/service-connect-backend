package com.serviceconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@DiscriminatorValue("client")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
 @NoArgsConstructor
 @AllArgsConstructor
 public class Client extends User {

     private String address;

     // Optional current location
     private Double locationLat;
     private Double locationLng;

     // Explicit getters
     public String getAddress() { return address; }
     public Double getLocationLat() { return locationLat; }
     public Double getLocationLng() { return locationLng; }
 }
