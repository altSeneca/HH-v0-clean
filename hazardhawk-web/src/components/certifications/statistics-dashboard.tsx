'use client';

import { motion } from 'framer-motion';
import { Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';

export interface StatisticsData {
  totalPending: number;
  approvedToday: number;
  rejectedToday: number;
  avgProcessingTimeMinutes: number;
}

interface StatisticsDashboardProps {
  data: StatisticsData;
}

/**
 * Statistics dashboard showing key certification metrics
 * Color-coded cards for pending (yellow), approved (green), rejected (red)
 */
export function StatisticsDashboard({ data }: StatisticsDashboardProps) {
  const stats = [
    {
      label: 'Pending Review',
      value: data.totalPending,
      icon: AlertCircle,
      color: 'yellow',
      bgColor: 'bg-yellow-50',
      textColor: 'text-yellow-700',
      iconColor: 'text-yellow-500',
      borderColor: 'border-yellow-200',
    },
    {
      label: 'Approved Today',
      value: data.approvedToday,
      icon: CheckCircle,
      color: 'green',
      bgColor: 'bg-green-50',
      textColor: 'text-green-700',
      iconColor: 'text-green-500',
      borderColor: 'border-green-200',
    },
    {
      label: 'Rejected Today',
      value: data.rejectedToday,
      icon: XCircle,
      color: 'red',
      bgColor: 'bg-red-50',
      textColor: 'text-red-700',
      iconColor: 'text-red-500',
      borderColor: 'border-red-200',
    },
    {
      label: 'Avg Processing Time',
      value: `${data.avgProcessingTimeMinutes}m`,
      icon: Clock,
      color: 'blue',
      bgColor: 'bg-blue-50',
      textColor: 'text-blue-700',
      iconColor: 'text-blue-500',
      borderColor: 'border-blue-200',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      {stats.map((stat, index) => {
        const Icon = stat.icon;
        return (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className={`${stat.bgColor} ${stat.borderColor} border-2 rounded-lg p-4 shadow-sm hover:shadow-md transition-shadow`}
          >
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-600 mb-1">{stat.label}</p>
                <p className={`text-3xl font-bold ${stat.textColor}`}>{stat.value}</p>
              </div>
              <div className={`${stat.iconColor} opacity-80`}>
                <Icon size={32} strokeWidth={2} />
              </div>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
