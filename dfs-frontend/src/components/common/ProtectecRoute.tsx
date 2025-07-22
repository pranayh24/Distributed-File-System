import React from "react";
import {useAuth} from "../../context/AuthContext.tsx";
import {AuthPage} from "../auth/AuthPage.tsx";

interface ProtectedRouteProps {
    children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    const {isAuthenticated, isLoading} = useAuth();

    if(isLoading) {
        return (
            <div className={"min-h-screen flex items-center justify-center"}>
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if(!isAuthenticated) {
        return <AuthPage />;
    }

    return <>{children}</>;
}