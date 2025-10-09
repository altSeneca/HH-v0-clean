'use client';

import { useState } from 'react';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
import { ZoomIn, ZoomOut, Maximize2, RotateCw, Home } from 'lucide-react';
import { motion } from 'framer-motion';

interface DocumentViewerProps {
  documentUrl: string;
  documentType?: 'image' | 'pdf';
}

/**
 * Document viewer with zoom, pan, and full-screen support
 * Works with both images and PDFs
 */
export function DocumentViewer({ documentUrl, documentType = 'image' }: DocumentViewerProps) {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [rotation, setRotation] = useState(0);
  const [imageLoaded, setImageLoaded] = useState(false);

  // Toggle fullscreen
  const toggleFullscreen = () => {
    if (!isFullscreen) {
      document.documentElement.requestFullscreen?.();
    } else {
      document.exitFullscreen?.();
    }
    setIsFullscreen(!isFullscreen);
  };

  // Rotate image
  const handleRotate = () => {
    setRotation((prev) => (prev + 90) % 360);
  };

  return (
    <div className={`flex flex-col h-full ${isFullscreen ? 'fixed inset-0 z-50 bg-black' : 'bg-gray-100'}`}>
      {/* Toolbar */}
      <div className={`flex items-center justify-between p-3 ${isFullscreen ? 'bg-gray-900' : 'bg-white'} border-b border-gray-300`}>
        <div className="flex items-center gap-2">
          <h3 className={`font-semibold ${isFullscreen ? 'text-white' : 'text-gray-900'}`}>
            Document Viewer
          </h3>
        </div>

        <TransformWrapper>
          {({ zoomIn, zoomOut, resetTransform }) => (
            <div className="flex items-center gap-2">
              <button
                onClick={() => zoomOut()}
                className={`p-2 rounded hover:bg-gray-200 ${isFullscreen ? 'text-white hover:bg-gray-700' : 'text-gray-700'}`}
                title="Zoom Out"
              >
                <ZoomOut size={20} />
              </button>
              <button
                onClick={() => zoomIn()}
                className={`p-2 rounded hover:bg-gray-200 ${isFullscreen ? 'text-white hover:bg-gray-700' : 'text-gray-700'}`}
                title="Zoom In"
              >
                <ZoomIn size={20} />
              </button>
              <button
                onClick={() => resetTransform()}
                className={`p-2 rounded hover:bg-gray-200 ${isFullscreen ? 'text-white hover:bg-gray-700' : 'text-gray-700'}`}
                title="Reset"
              >
                <Home size={20} />
              </button>
              <button
                onClick={handleRotate}
                className={`p-2 rounded hover:bg-gray-200 ${isFullscreen ? 'text-white hover:bg-gray-700' : 'text-gray-700'}`}
                title="Rotate"
              >
                <RotateCw size={20} />
              </button>
              <div className={`w-px h-6 ${isFullscreen ? 'bg-gray-700' : 'bg-gray-300'}`}></div>
              <button
                onClick={toggleFullscreen}
                className={`p-2 rounded hover:bg-gray-200 ${isFullscreen ? 'text-white hover:bg-gray-700' : 'text-gray-700'}`}
                title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
              >
                <Maximize2 size={20} />
              </button>
            </div>
          )}
        </TransformWrapper>
      </div>

      {/* Document Display */}
      <div className="flex-1 overflow-hidden relative">
        <TransformWrapper
          initialScale={1}
          minScale={0.5}
          maxScale={5}
          centerOnInit
          wheel={{ step: 0.1 }}
          doubleClick={{ mode: 'reset' }}
        >
          {({ zoomIn, zoomOut, resetTransform, ...rest }) => (
            <TransformComponent
              wrapperClass="w-full h-full"
              contentClass="w-full h-full flex items-center justify-center"
            >
              <div className="relative">
                {/* Loading spinner */}
                {!imageLoaded && (
                  <div className="absolute inset-0 flex items-center justify-center bg-gray-100">
                    <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                  </div>
                )}

                {/* Image */}
                {documentType === 'image' ? (
                  <motion.img
                    src={documentUrl}
                    alt="Certification Document"
                    className="max-w-full max-h-full object-contain"
                    style={{ transform: `rotate(${rotation}deg)` }}
                    onLoad={() => setImageLoaded(true)}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: imageLoaded ? 1 : 0 }}
                    transition={{ duration: 0.3 }}
                  />
                ) : (
                  /* PDF - Use iframe or embed */
                  <iframe
                    src={documentUrl}
                    className="w-full h-full min-h-[600px]"
                    title="PDF Document"
                    onLoad={() => setImageLoaded(true)}
                  />
                )}
              </div>
            </TransformComponent>
          )}
        </TransformWrapper>

        {/* Zoom instructions overlay */}
        {!isFullscreen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: imageLoaded ? 0.8 : 0 }}
            transition={{ delay: 1, duration: 0.5 }}
            className="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-gray-900 text-white text-xs px-3 py-2 rounded shadow-lg pointer-events-none"
          >
            Scroll to zoom • Drag to pan • Double-click to reset
          </motion.div>
        )}
      </div>
    </div>
  );
}
