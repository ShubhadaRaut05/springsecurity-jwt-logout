package com.shubhada.twofactorauthentication.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private  String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    public String extractUsername(String token) {
        return extractClaim(token,Claims::getSubject);
        //subject should be email or username

    }
    public String generateToken(UserDetails userDetails)
    {

        return generateToken(new HashMap<>(),userDetails);
    }
    public String generateToken(
            //Map object contains claims ans extra claims
            Map<String,Object> extraClaims,
            UserDetails userDetails
    )
    {
        return buildToken(extraClaims,userDetails,jwtExpiration);
    }
    public String generateRefreshToken(
            //Map object contains claims ans extra claims
            UserDetails userDetails
    )
    {
        return buildToken(new HashMap<>(),userDetails,refreshExpiration);
    }
    private String buildToken(
            Map<String,Object> extraClaims,
            UserDetails userDetails,
            long expiration
    )
    {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();//generate and return token
    }
    public boolean isTokenValid(String token,UserDetails userDetails)
    {
        final String username=extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token,Claims::getExpiration);
    }

    private Claims extractAllClaims(String token)
    {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())   //we need sign-in key when we decode token
                .build()
                .parseClaimsJws(token)
                .getBody()
                ;
    }
public <T> T extractClaim(String token, Function<Claims,T> claimsResolver)
{
    final Claims claims=extractAllClaims(token);
    return claimsResolver.apply(claims);
}
    private Key getSignInKey() {
        byte[] keyBytes= Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
