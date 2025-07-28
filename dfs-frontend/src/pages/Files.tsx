import React, { useState } from 'react';
import { Layout } from '../components/layout/Layout';
import { FileManager } from '../components/files/FileManager';

const Files: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');

    const handleSearch = (query: string) => {
        setSearchQuery(query);
    };

    return (
        <Layout onSearch={handleSearch}>
            <FileManager searchQuery={searchQuery} />
        </Layout>
    );
};

export default Files;
