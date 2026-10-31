package com.site.server.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    private final String headerName;

    public ClientIpResolver(@Value("${app.client-ip-header:}") String headerName) {
        this.headerName = headerName.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (!headerName.isEmpty()) {
            var headers = request.getHeaders(headerName);
            if (headers != null && headers.hasMoreElements()) {
                String value = headers.nextElement();
                if (value != null && !headers.hasMoreElements()) {
                    String address = literalAddress(value.trim());
                    if (address != null) return address;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private String literalAddress(String value) {
        if (value.length() > 45 || value.isEmpty()) return null;
        if (!value.contains(":")) return validIpv4(value) ? value : null;
        // Restrict to IPv6 literal characters before parsing: never resolve a hostname.
        // Lists, ports in brackets, scope IDs and duplicate header lines are not trusted.
        if (!value.matches("[0-9a-fA-F:.]+")) return null;
        if (value.contains(".") && !validIpv4(value.substring(value.lastIndexOf(':') + 1))) return null;
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private boolean validIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) return false;
        }
        return true;
    }
}
