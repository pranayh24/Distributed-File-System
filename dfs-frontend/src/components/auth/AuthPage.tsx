import {LoginForm} from "./LoginForm.tsx";
import {RegisterForm} from "./RegisterForm.tsx";
import React from "react";

export const AuthPage: React.FC = () => {
    const [ isLogin, setIsLogin ] = React.useState(false);

    return (
        <>
            {isLogin} ? (
                <LoginForm onSwitchToRegister={() => setIsLogin((false))} />
            ) : (
                <RegisterForm onSwitchToLogin={() => setIsLogin((true))} />
        </>
    )
}