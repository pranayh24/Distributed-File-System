import React from "react";
import { Download, Shield, Upload, ArrowRight } from "lucide-react";
import { Layout } from "../components/layout/Layout.tsx";
import { Link } from "react-router-dom";

export const Home: React.FC = () => {
    return (
        <Layout>
            <div className="text-center py-16 px-4">
                <h1 className="text-5xl font-extrabold text-gray-900 dark:text-white mb-4 tracking-tight">
                    A Modern Distributed File System
                </h1>
                <p className="text-xl text-gray-600 dark:text-gray-300 mb-10 max-w-2xl mx-auto">
                    Securely store, manage, and access your files from anywhere with our fault-tolerant, replicated, and version-controlled storage solution.
                </p>
                <Link to="/files" className="inline-flex items-center justify-center px-8 py-3 text-base font-medium text-white bg-primary-600 border border-transparent rounded-md shadow-sm hover:bg-primary-700">
                    Go to Your Files <ArrowRight className="ml-2 -mr-1 h-5 w-5" />
                </Link>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mt-16">
                <div className="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 transform hover:scale-105 transition-transform duration-300">
                    <div className="flex items-center justify-center h-12 w-12 rounded-full bg-primary-100 dark:bg-primary-900/50 mb-4">
                        <Upload className="h-6 w-6 text-primary-600 dark:text-primary-400" />
                    </div>
                    <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Upload with Confidence</h3>
                    <p className="text-gray-600 dark:text-gray-300 text-sm">
                        Securely upload your files with automatic replication for data durability.
                    </p>
                </div>

                <div className="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 transform hover:scale-105 transition-transform duration-300">
                    <div className="flex items-center justify-center h-12 w-12 rounded-full bg-green-100 dark:bg-green-900/50 mb-4">
                        <Download className="h-6 w-6 text-green-600 dark:text-green-400" />
                    </div>
                    <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Access Anywhere</h3>
                    <p className="text-gray-600 dark:text-gray-300 text-sm">
                        Download your files from any device, anytime, with our distributed network.
                    </p>
                </div>

                <div className="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 transform hover:scale-105 transition-transform duration-300">
                    <div className="flex items-center justify-center h-12 w-12 rounded-full bg-red-100 dark:bg-red-900/50 mb-4">
                        <Shield className="h-6 w-6 text-red-600 dark:text-red-400" />
                    </div>
                    <h3 className="text-lg font-semibold mb-2 text-gray-900 dark:text-white">Secure & Reliable</h3>
                    <p className="text-gray-600 dark:text-gray-300 text-sm">
                        End-to-end encryption and fault tolerance ensure your data is always safe.
                    </p>
                </div>
            </div>
        </Layout>
    );
};