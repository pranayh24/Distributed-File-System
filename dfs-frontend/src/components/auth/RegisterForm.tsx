import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Eye, EyeOff, UserPlus } from 'lucide-react';

interface RegisterFormProps {
    onSwitchToLogin : () => void;
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ onSwitchToLogin }) => {
    const { register, isLoading, error, clearError } = useAuth();
    const [ formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if(error) clearError();
    };

    const handleSubmit = async (e : React.FormEvent) => {
        e.preventDefault();

        if(formData.password !== formData.confirmPassword) {
            return;
        }

        try {
            await register({
                username: formData.username,
                email: formData.email,
                password: formData.password,
            });
        } catch (error) {

        }
    };

    const  isFormValid = formData.username && formData.email && formData.password &&
                                formData.confirmPassword && formData.password === formData.confirmPassword;

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8">
                <div>
                    <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-primary-600">
                        <UserPlus className="h-6 w-6 text-white" />
                    </div>
                    <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
                        Create your account
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        Join the distributed file system
                    </p>
                </div>

                <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
                    {error && (
                        <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-md">
                            {error}
                        </div>
                    )}

                    <div className="space-y-4">
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                                Username
                            </label>
                            <input
                                id="username"
                                name="username"
                                type="text"
                                required
                                value={formData.username}
                                onChange={handleChange}
                                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                                placeholder="Choose a username"
                            />
                        </div>

                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                                Email Address
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                required
                                value={formData.email}
                                onChange={handleChange}
                                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                                placeholder="Enter your email"
                            />
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                                Password
                            </label>
                            <div className="mt-1 relative">
                                <input
                                    id="password"
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    required
                                    value={formData.password}
                                    onChange={handleChange}
                                    className="appearance-none relative block w-full px-3 py-2 pr-10 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                                    placeholder="Create a password"
                                />
                                <button
                                    type="button"
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                                    onClick={() => setShowPassword(!showPassword)}
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-4 w-4 text-gray-400" />
                                    ) : (
                                        <Eye className="h-4 w-4 text-gray-400" />
                                    )}
                                </button>
                            </div>
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                                Confirm Password
                            </label>
                            <div className="mt-1 relative">
                                <input
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    required
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    className={`appearance-none relative block w-full px-3 py-2 pr-10 border placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm ${
                                        formData.confirmPassword && formData.password !== formData.confirmPassword
                                            ? 'border-red-300'
                                            : 'border-gray-300'
                                    }`}
                                    placeholder="Confirm your password"
                                />
                                <button
                                    type="button"
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                >
                                    {showConfirmPassword ? (
                                        <EyeOff className="h-4 w-4 text-gray-400" />
                                    ) : (
                                        <Eye className="h-4 w-4 text-gray-400" />
                                    )}
                                </button>
                            </div>
                            {formData.confirmPassword && formData.password !== formData.confirmPassword && (
                                <p className="mt-1 text-sm text-red-600">Passwords do not match</p>
                            )}
                        </div>
                    </div>

                    <div>
                        <button
                            type="submit"
                            disabled={isLoading || !isFormValid}
                            className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isLoading ? 'Creating account...' : 'Create account'}
                        </button>
                    </div>

                    <div className="text-center">
                        <span className="text-gray-600">Already have an account? </span>
                        <button
                            type="button"
                            onClick={onSwitchToLogin}
                            className="font-medium text-primary-600 hover:text-primary-500"
                        >
                            Sign in here
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};