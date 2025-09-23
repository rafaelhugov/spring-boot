//package mppb.microservices.common.infrastructure.interceptor;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import java.util.concurrent.TimeUnit;
//
//@Component
//public class CacheControlInterceptor implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // Define a política de cache para 15 minutos
//        String cacheValue = "max-age=" + TimeUnit.HOURS.toSeconds(1) + ", must-revalidate";
//        response.setHeader("Cache-Control", cacheValue);
//        return true; // Continua a execução
//    }
//}
