toll.tag.event → VerifyService.process()
→ publishes toll.charge.request
→ PaymentProcessor.onRequest()  (with retries + DLT)
→ publishes toll.charge.response
→ VerifyService.applyPaymentResult()
→ GateCommand → gate-service
