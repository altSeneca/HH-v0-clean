'use client';

import { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CertificationRecord } from '@/types/api';
import { Filter, ArrowUpDown, User } from 'lucide-react';
import { format } from 'date-fns';

interface VerificationQueueProps {
  certifications: CertificationRecord[];
  selectedId?: string | null;
  onSelect: (certification: CertificationRecord) => void;
}

type SortField = 'date' | 'type' | 'confidence';
type SortDirection = 'asc' | 'desc';
type FilterType = 'all' | 'high' | 'medium' | 'low';

/**
 * List of pending certifications with filter/sort controls
 * Supports clicking to select and view details
 */
export function VerificationQueue({ certifications, selectedId, onSelect }: VerificationQueueProps) {
  const [sortField, setSortField] = useState<SortField>('date');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [confidenceFilter, setConfidenceFilter] = useState<FilterType>('all');
  const [searchTerm, setSearchTerm] = useState('');

  // Get confidence badge
  const getConfidenceBadge = (confidence: number) => {
    if (confidence >= 85) {
      return { color: 'bg-green-500', label: 'High', level: 'high' as const };
    } else if (confidence >= 60) {
      return { color: 'bg-amber-500', label: 'Medium', level: 'medium' as const };
    } else {
      return { color: 'bg-red-500', label: 'Low', level: 'low' as const };
    }
  };

  // Filter and sort certifications
  const filteredAndSorted = useMemo(() => {
    let filtered = [...certifications];

    // Apply confidence filter
    if (confidenceFilter !== 'all') {
      filtered = filtered.filter((cert) => {
        const badge = getConfidenceBadge(cert.ocrConfidence);
        return badge.level === confidenceFilter;
      });
    }

    // Apply search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (cert) =>
          cert.holderName.toLowerCase().includes(term) ||
          cert.certificationType.toLowerCase().includes(term) ||
          cert.certificationNumber.toLowerCase().includes(term)
      );
    }

    // Sort
    filtered.sort((a, b) => {
      let compareValue = 0;

      switch (sortField) {
        case 'date':
          compareValue = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
          break;
        case 'type':
          compareValue = a.certificationType.localeCompare(b.certificationType);
          break;
        case 'confidence':
          compareValue = a.ocrConfidence - b.ocrConfidence;
          break;
      }

      return sortDirection === 'asc' ? compareValue : -compareValue;
    });

    return filtered;
  }, [certifications, sortField, sortDirection, confidenceFilter, searchTerm]);

  // Toggle sort
  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-gray-200 bg-white">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Verification Queue</h2>

        {/* Search */}
        <input
          type="text"
          placeholder="Search by name, type, or number..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 mb-3"
        />

        {/* Controls */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Sort buttons */}
          <div className="flex items-center gap-1">
            <button
              onClick={() => toggleSort('date')}
              className={`flex items-center gap-1 px-3 py-1.5 text-sm rounded ${
                sortField === 'date' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'
              } hover:bg-blue-50 transition-colors`}
            >
              Date
              {sortField === 'date' && (
                <ArrowUpDown size={14} className={sortDirection === 'asc' ? 'rotate-180' : ''} />
              )}
            </button>
            <button
              onClick={() => toggleSort('type')}
              className={`flex items-center gap-1 px-3 py-1.5 text-sm rounded ${
                sortField === 'type' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'
              } hover:bg-blue-50 transition-colors`}
            >
              Type
              {sortField === 'type' && (
                <ArrowUpDown size={14} className={sortDirection === 'asc' ? 'rotate-180' : ''} />
              )}
            </button>
            <button
              onClick={() => toggleSort('confidence')}
              className={`flex items-center gap-1 px-3 py-1.5 text-sm rounded ${
                sortField === 'confidence' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'
              } hover:bg-blue-50 transition-colors`}
            >
              Confidence
              {sortField === 'confidence' && (
                <ArrowUpDown size={14} className={sortDirection === 'asc' ? 'rotate-180' : ''} />
              )}
            </button>
          </div>

          {/* Confidence filter */}
          <div className="flex items-center gap-1 ml-auto">
            <Filter size={16} className="text-gray-500" />
            <select
              value={confidenceFilter}
              onChange={(e) => setConfidenceFilter(e.target.value as FilterType)}
              className="text-sm border border-gray-300 rounded px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">All Confidence</option>
              <option value="high">High (85%+)</option>
              <option value="medium">Medium (60-85%)</option>
              <option value="low">Low (&lt;60%)</option>
            </select>
          </div>
        </div>

        {/* Result count */}
        <p className="text-sm text-gray-600 mt-3">
          {filteredAndSorted.length} of {certifications.length} certifications
        </p>
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto">
        <AnimatePresence initial={false}>
          {filteredAndSorted.length === 0 ? (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="flex flex-col items-center justify-center h-full p-8 text-center"
            >
              <Filter size={48} className="text-gray-300 mb-4" />
              <p className="text-gray-600 font-medium">No certifications match your filters</p>
              <p className="text-gray-500 text-sm mt-2">Try adjusting your search or filters</p>
            </motion.div>
          ) : (
            <div className="divide-y divide-gray-200">
              {filteredAndSorted.map((cert, index) => {
                const badge = getConfidenceBadge(cert.ocrConfidence);
                const isSelected = cert.id === selectedId;

                return (
                  <motion.div
                    key={cert.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -20 }}
                    transition={{ delay: index * 0.02 }}
                    onClick={() => onSelect(cert)}
                    className={`p-4 cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-blue-50 border-l-4 border-blue-500'
                        : 'bg-white hover:bg-gray-50 border-l-4 border-transparent'
                    }`}
                  >
                    {/* Worker name and confidence */}
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <User size={18} className="text-gray-500" />
                        <h3 className="font-semibold text-gray-900">{cert.holderName}</h3>
                      </div>
                      <div className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${badge.color} bg-opacity-10`}>
                        <div className={`w-2 h-2 rounded-full ${badge.color}`}></div>
                        {badge.label} {Math.round(cert.ocrConfidence)}%
                      </div>
                    </div>

                    {/* Cert type */}
                    <p className="text-sm font-medium text-gray-700 mb-1">{cert.certificationType}</p>

                    {/* Cert number */}
                    <p className="text-xs font-mono text-gray-500 mb-2">{cert.certificationNumber}</p>

                    {/* Date */}
                    <p className="text-xs text-gray-500">
                      Uploaded {format(new Date(cert.createdAt), 'MMM d, yyyy h:mm a')}
                    </p>
                  </motion.div>
                );
              })}
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
