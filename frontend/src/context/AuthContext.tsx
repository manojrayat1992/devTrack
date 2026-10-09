import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import Keycloak from 'keycloak-js';
import { UserProfile } from '../types';

interface AuthContextType {
  authenticated: boolean;
  loading: boolean;
  user: UserProfile | null;
  token: string | null;
  login: () => void;
  logout: () => void;
  keycloakInstance: Keycloak | null;
  authError: string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'devtrack',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'devtrack-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [authenticated, setAuthenticated] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .then((auth) => {
        if (!isMounted) return;
        setAuthenticated(auth);
        if (auth && keycloak.token) {
          setToken(keycloak.token);
          extractUserProfile();
        }
        setLoading(false);
      })
      .catch((err) => {
        if (!isMounted) return;
        console.warn('Keycloak initialization note (Keycloak may still be starting up):', err);
        setAuthError('Keycloak authentication server is connecting or offline.');
        setLoading(false);
      });

    keycloak.onTokenExpired = () => {
      keycloak
        .updateToken(30)
        .then((refreshed) => {
          if (refreshed && keycloak.token) {
            setToken(keycloak.token);
          }
        })
        .catch(() => {
          console.error('Failed to refresh token, logging out');
          setAuthenticated(false);
          setUser(null);
          setToken(null);
        });
    };

    return () => {
      isMounted = false;
    };
  }, []);

  const extractUserProfile = () => {
    if (!keycloak.tokenParsed) return;
    const parsed = keycloak.tokenParsed;
    const realmAccess = (parsed as { realm_access?: { roles?: string[] } }).realm_access;
    const roles: string[] = realmAccess?.roles || [];
    const isAdmin = roles.includes('ADMIN');

    const profile: UserProfile = {
      id: parsed.sub || '',
      username: (parsed as { preferred_username?: string }).preferred_username || parsed.sub || 'User',
      email: (parsed as { email?: string }).email,
      firstName: (parsed as { given_name?: string }).given_name,
      lastName: (parsed as { family_name?: string }).family_name,
      roles,
      isAdmin,
    };
    setUser(profile);
  };

  const login = () => {
    keycloak.login({
      redirectUri: window.location.origin,
    });
  };

  const logout = () => {
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  };

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        loading,
        user,
        token,
        login,
        logout,
        keycloakInstance: keycloak,
        authError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
