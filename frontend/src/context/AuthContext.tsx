import React, { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import Keycloak from 'keycloak-js';
import { UserProfile } from '../types';
import { setAuthToken, setTokenSupplier } from '../api/apiClient';

interface AuthContextType {
  authenticated: boolean;
  loading: boolean;
  user: UserProfile | null;
  token: string | null;
  login: () => void;
  register: () => void;
  logout: () => void;
  getToken: () => Promise<string | null>;
  keycloakInstance: Keycloak | null;
  authError: string | null;
  clearAuthError: () => void;
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

  const extractUserProfile = useCallback(() => {
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
  }, []);

  const getToken = useCallback(async (): Promise<string | null> => {
    if (!keycloak.authenticated) {
      return null;
    }
    try {
      // Refresh token if within 30 seconds of expiration
      const refreshed = await keycloak.updateToken(30);
      if (refreshed && keycloak.token) {
        setToken(keycloak.token);
        setAuthToken(keycloak.token);
      }
      return keycloak.token || null;
    } catch (err) {
      console.warn('Failed to refresh access token, session may be expired:', err);
      setAuthenticated(false);
      setUser(null);
      setToken(null);
      setAuthToken(null);
      setAuthError('Your authentication session has expired. Please sign in again.');
      return null;
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    // Connect token supplier with API client
    setTokenSupplier(getToken);

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
          setAuthToken(keycloak.token);
          extractUserProfile();
        }
        setLoading(false);
      })
      .catch((err) => {
        if (!isMounted) return;
        console.warn('Keycloak initialization note (Keycloak may still be starting up):', err);
        setAuthError('Keycloak authentication server is connecting or offline. Verify Docker services are running.');
        setLoading(false);
      });

    keycloak.onTokenExpired = () => {
      keycloak
        .updateToken(30)
        .then((refreshed) => {
          if (refreshed && keycloak.token) {
            setToken(keycloak.token);
            setAuthToken(keycloak.token);
          }
        })
        .catch(() => {
          console.warn('Session expired. Redirecting to unauthenticated state.');
          setAuthenticated(false);
          setUser(null);
          setToken(null);
          setAuthToken(null);
          setAuthError('Your authentication session has expired. Please sign in again.');
        });
    };

    return () => {
      isMounted = false;
    };
  }, [extractUserProfile, getToken]);

  const login = () => {
    keycloak.login({
      redirectUri: window.location.origin,
    });
  };

  const register = () => {
    keycloak.register({
      redirectUri: window.location.origin,
    });
  };

  const logout = () => {
    setAuthToken(null);
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  };

  const clearAuthError = () => {
    setAuthError(null);
  };

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        loading,
        user,
        token,
        login,
        register,
        logout,
        getToken,
        keycloakInstance: keycloak,
        authError,
        clearAuthError,
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
