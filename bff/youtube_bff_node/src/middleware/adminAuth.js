export function requireAdminToken(req, res, next) {
  const configuredToken = process.env.ADMIN_TOKEN?.trim();

  if (!configuredToken) {
    if (process.env.NODE_ENV === "production") {
      return res.status(503).json({
        error: "ADMIN_TOKEN must be configured before admin endpoints can be used in production."
      });
    }

    return next();
  }

  const providedToken = req.get("x-admin-token") || parseBearerToken(req.get("authorization"));

  if (providedToken !== configuredToken) {
    return res.status(401).json({ error: "Invalid admin token." });
  }

  return next();
}

function parseBearerToken(header) {
  if (!header?.startsWith("Bearer ")) {
    return "";
  }

  return header.slice("Bearer ".length).trim();
}
