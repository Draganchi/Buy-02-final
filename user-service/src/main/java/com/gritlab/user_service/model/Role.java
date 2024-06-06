package com.gritlab.user_service.model;

public enum Role {
    SELLER, CLIENT;
    @Override
    public String toString() {
        return "ROLE_" + this.name();
    }
}
