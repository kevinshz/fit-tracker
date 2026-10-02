package com.fittracker.common.exceptions;

import java.util.Map;

public record ErrorResponse(Map<String, String> error) {

}
