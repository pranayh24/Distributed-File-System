import React from "react";
import {Download, Shield, Upload} from "lucide-react";
import {Layout} from "../components/layout/Layout.tsx";

export const Home: React.FC = ()=> {
    return (
        <Layout>
            <div className="text-center py-12">
                <h2 className="text-3xl font-bold text-gray-900 mb-4">
                    Welcome to DFS
                </h2>
                <p className="text-lg text-gray-600 mb-8">
                    Your secure, distributed file storage solution
                </p>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-12">
                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <Upload className="h-8 w-8 text-primary-600 mx-auto mb-4" />
                        <h3 className="text-lg font-semibold mb-2">Upload Files</h3>
                        <p className="text-gray-600 text-sm">
                            Securely upload your files with automatic replication
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <Download className="h-8 w-8 text-primary-600 mx-auto mb-4" />
                        <h3 className="text-lg font-semibold mb-2">Download Files</h3>
                        <p className="text-gray-600 text-sm">
                            Access your files from anywhere, anytime
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <Shield className="h-8 w-8 text-primary-600 mx-auto mb-4" />
                        <h3 className="text-lg font-semibold mb-2">Secure</h3>
                        <p className="text-gray-600 text-sm">
                            End-to-end encryption and fault tolerance
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-lg shadow-sm border">
                        <button className="btn-primary mr-4">
                            Get Started
                        </button>
                        <button className="btn-secondary">
                            Learn More
                        </button>
                    </div>
                </div>
            </div>
        </Layout>
    );
};