# TODO Steps for Updating PlanController

- [x] Update getAllPublishedPlans endpoint: Add try-catch block to handle exceptions and return proper error messages.
- [x] Update createPlan endpoint: Add try-catch for IllegalArgumentException and return badRequest with error message.
- [x] Update updatePlan endpoint: Check if plan exists using service.getPlanById; if not, return notFound; else try save and catch exceptions.
- [x] Update deletePlan endpoint: Check if plan exists; if not, return notFound; else delete.
- [x] Test all endpoints to verify error messages are returned properly.
