#!/bin/bash

echo "=== TICKET SERVICE HEALTH CHECK ===" 
echo "Timestamp: $(date)"
echo ""

echo "1. HPA Status:"
kubectl get hpa ticket-service-hpa -o wide
echo ""

echo "2. Deployment Status:"
kubectl get deployment ticket-service -o wide
echo ""

echo "3. Running Pods:"
kubectl get pods | grep ticket-service | grep Running
echo ""

echo "4. Evicted Pods Count:"
EVICTED_COUNT=$(kubectl get pods | grep ticket-service | grep Evicted | wc -l)
echo "Total Evicted: $EVICTED_COUNT"
echo ""

echo "5. Resource Usage:"
kubectl top pods | grep ticket-service | head -5
echo ""

echo "6. Recent Events:"
kubectl get events --sort-by=.metadata.creationTimestamp | grep ticket-service | tail -5
echo ""

echo "7. HPA Metrics Detail:"
kubectl describe hpa ticket-service-hpa | grep -A 10 "Metrics:"
echo ""

echo "=== SUMMARY ===" 
RUNNING_COUNT=$(kubectl get pods | grep ticket-service | grep Running | wc -l)
ERROR_COUNT=$(kubectl get pods | grep ticket-service | grep Error | wc -l)

echo "✅ Running Pods: $RUNNING_COUNT"
echo "❌ Evicted Pods: $EVICTED_COUNT"
echo "🔥 Error Pods: $ERROR_COUNT"

if [ $RUNNING_COUNT -ge 3 ] && [ $EVICTED_COUNT -eq 0 ]; then
    echo "🎉 HEALTH STATUS: GOOD"
else
    echo "⚠️  HEALTH STATUS: NEEDS ATTENTION"
fi 