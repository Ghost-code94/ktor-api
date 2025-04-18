package grpc.ghostcache.auth

import io.grpc.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.logging.Logger

class JwtAuthInterceptor(secretKey: String) : ServerInterceptor {
  private val parser = Jwts.parserBuilder()
    .setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray()))
    .build()
  private val log = Logger.getLogger("JwtAuthInterceptor")

  companion object {
    // Context key under which we’ll store claims
    val JWT_CLAIMS_CTX_KEY: Context.Key<io.jsonwebtoken.Claims> =
        Context.key("jwt_claims")
  }

  override fun <ReqT, RespT> interceptCall(
      call: ServerCall<ReqT, RespT>,
      headers: Metadata,
      next: ServerCallHandler<ReqT, RespT>
  ): ServerCall.Listener<ReqT> {
    val auth = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER))
    if (auth == null || !auth.startsWith("Bearer ")) {
      call.close(Status.UNAUTHENTICATED.withDescription("No Bearer token"), headers)
      return object : ServerCall.Listener<ReqT>() {}
    }

    try {
      val token = auth.removePrefix("Bearer ").trim()
      val claims = parser.parseClaimsJws(token).body
      // embed claims into context so your service impl can read them:
      val ctx = Context.current().withValue(JWT_CLAIMS_CTX_KEY, claims)
      return Contexts.interceptCall(ctx, call, headers, next)
    } catch (e: Exception) {
      log.warning("JWT validation failed: ${e.message}")
      call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), headers)
      return object : ServerCall.Listener<ReqT>() {}
    }
  }
}
