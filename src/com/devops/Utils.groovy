package com.example

class Utils {
    static String getCurrentTimestamp() {
        return new Date().format('yyyy-MM-dd HH:mm:ss')
    }
    
    static void notifySlack(String message, String channel = '#general') {
        // Implementation for Slack notification
        echo "Slack notification: ${message} to ${channel}"
    }
}