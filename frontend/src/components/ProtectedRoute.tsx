import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LoadingScreen } from './LoadingScreen';
import { UnauthorizedScreen } from './UnauthorizedScreen';

interface ProtectedRouteProps {
  children: React.ReactElement;
  requiredRole?: 'ADMIN' | 'USER';
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRole,
}) => {
  const { authenticated, loading, user } = useAuth();

  if (loading) {
    return <LoadingScreen message="Verifying authentication session..." />;
  }

  if (!authenticated) {
    return (
      <UnauthorizedScreen
        type="unauthenticated"
        title="Authentication Required"
        description="Please sign in or create an account via Keycloak OpenID Connect to access this protected area."
      />
    );
  }

  if (requiredRole === 'ADMIN' && !user?.isAdmin) {
    return (
      <UnauthorizedScreen
        type="forbidden"
        requiredRole="ADMIN"
        title="Administrator Access Denied"
        description={`Your account '${user?.username}' does not have the administrative role required to inspect system telemetry.`}
      />
    );
  }

  return children;
};
