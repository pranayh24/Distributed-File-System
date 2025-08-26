import React, { useState } from 'react';
import { Upload, FolderOpen } from 'lucide-react';
import { useDragDrop } from '../../hooks/useDragDrop';

interface DropZoneProps {
    targetPath: string;
    onDrop: (result: any) => void;
    children?: React.ReactNode;
    className?: string;
    showIndicator?: boolean;
    acceptsFiles?: boolean;
    acceptsDirectories?: boolean;
}

export const DropZone: React.FC<DropZoneProps> = ({
                                                      targetPath,
                                                      onDrop,
                                                      children,
                                                      className = '',
                                                      showIndicator = true,
                                                      acceptsFiles = true,
                                                      acceptsDirectories = true
                                                  }) => {
    const { handleDragOver, handleDragLeave, handleDrop, dragOver } = useDragDrop();
    const [isDragOverZone, setIsDragOverZone] = useState(false);

    const onDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragOverZone(true);
        handleDragOver(e, targetPath);
    };

    const onDragLeave = (e: React.DragEvent) => {
        setIsDragOverZone(false);
        handleDragLeave(e);
    };

    const onDropHandler = async (e: React.DragEvent) => {
        setIsDragOverZone(false);
        const result = await handleDrop(e, targetPath);
        onDrop(result);
    };

    const isActive = dragOver === targetPath || isDragOverZone;

    return (
        <div
            onDragOver={onDragOver}
            onDragLeave={onDragLeave}
            onDrop={onDropHandler}
            className={`
                relative transition-all duration-200
                ${isActive ? 'scale-[1.02]' : ''}
                ${className}
            `}
        >
            {children}

            {/* Drop Indicator */}
            {showIndicator && isActive && (
                <div className="absolute inset-0 bg-blue-500/10 border-2 border-dashed border-blue-500 rounded-lg flex items-center justify-center z-10 pointer-events-none">
                    <div className="bg-blue-500 text-white px-4 py-2 rounded-lg shadow-lg flex items-center gap-2">
                        {acceptsFiles && !acceptsDirectories && <Upload size={20} />}
                        {acceptsDirectories && !acceptsFiles && <FolderOpen size={20} />}
                        {acceptsFiles && acceptsDirectories && <Upload size={20} />}
                        <span className="font-medium">
                            {acceptsFiles && acceptsDirectories ? 'Drop here' :
                                acceptsFiles ? 'Drop files here' :
                                    'Drop folders here'}
                        </span>
                    </div>
                </div>
            )}
        </div>
    );
};