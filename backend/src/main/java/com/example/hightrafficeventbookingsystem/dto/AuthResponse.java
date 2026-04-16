package com.example.hightrafficeventbookingsystem.dto;

public record AuthResponse(
	String token,
	String refreshToken
) {
}
