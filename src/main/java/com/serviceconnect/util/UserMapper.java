package com.serviceconnect.util;

import com.serviceconnect.dto.response.UserResponse;
import com.serviceconnect.entity.Client;
import com.serviceconnect.entity.Technician;
import com.serviceconnect.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setAvatar(user.getAvatar());
        response.setCreatedAt(user.getCreatedAt());

        if (user instanceof Technician t) {
            response.setServiceTypes(t.getServiceTypes());
            response.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
            response.setAccountStatus(t.getAccountStatus() != null ? t.getAccountStatus().name() : null);
            response.setAvailability(t.getAvailability() != null ? t.getAvailability().name() : null);
            response.setRating(t.getRating());
            response.setTotalJobs(t.getTotalJobs());
            response.setDocuments(t.getDocuments());
            response.setSubscriptionExpiryAt(t.getSubscriptionExpiryAt());
            response.setRegion(t.getRegion());
            response.setDistrict(t.getDistrict());
            response.setStreet(t.getStreet());

            // Legacy location data (optional, for backward compatibility)
            if (t.getLocationLat() != null) {
                UserResponse.LocationData location = new UserResponse.LocationData();
                location.setLat(t.getLocationLat());
                location.setLng(t.getLocationLng());
                location.setAddress(t.getLocationAddress());
                response.setLocation(location);
            }
        }

        if (user instanceof Client c) {
            response.setAddress(c.getAddress());
            if (c.getLocationLat() != null) {
                UserResponse.LocationData location = new UserResponse.LocationData();
                location.setLat(c.getLocationLat());
                location.setLng(c.getLocationLng());
                response.setLocation(location);
            }
        }

        return response;
    }
}
