import React from 'react';
import { useDragDrop } from '../../hooks/useDragDrop';
import type { FileItem } from './types';

interface DraggableProps {
    file: FileItem;
    children: React.ReactNode;
    className?: string;
}

export const Draggable: React.FC<DraggableProps> = ({
                                                        file,
                                                        children,
                                                        className = ''
                                                    }) => {
    const { handleDragStart, handleDragEnd } = useDragDrop();

    const onDragStart = (e: React.DragEvent) => {
        handleDragStart({
            id: file.id,
            name: file.name,
            path: file.path,
            isDirectory: file.isDirectory,
            type: file.isDirectory ? 'directory' : 'file'
        }, e);
    };

    return (
        <div
            draggable
            onDragStart={onDragStart}
            onDragEnd={handleDragEnd}
            className={`${className} cursor-move`}
        >
            {children}
        </div>
    );
};