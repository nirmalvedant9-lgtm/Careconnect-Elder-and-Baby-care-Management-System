package com.example.careconnect;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.IOException;
import java.util.List;
public class TokenFilter extends OncePerRequestFilter {
 private final Api api;
 public TokenFilter(Api api){this.api=api;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String h=req.getHeader("Authorization");
  if(h!=null&&h.startsWith("Bearer ")){
   try{
    var user=api.tokenUser(h.substring(7));
    var auth=new UsernamePasswordAuthenticationToken(user,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role())));
    SecurityContextHolder.getContext().setAuthentication(auth);
   }catch(Exception e){SecurityContextHolder.clearContext();}
  }
  chain.doFilter(req,res);
 }
}
