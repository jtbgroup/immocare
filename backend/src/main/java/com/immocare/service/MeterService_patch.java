// ─── In MeterService.java — only the changed methods are shown ───────────────
// Replace the corresponding methods in your existing MeterService.java

    @Transactional
    public MeterDTO addMeter(String ownerType, Long ownerId, AddMeterRequest request) {
        validateOwnerExists(ownerType, ownerId);
        validateMeterType(request.type());
        validateStartDateNotFuture(request.startDate());
        validateConditionalFields(request.type(), ownerType,
                request.eanCode(), request.installationNumber(), request.customerNumber());

        Meter meter = new Meter();
        meter.setType(request.type());
        meter.setMeterNumber(request.meterNumber());
        meter.setLabel(request.label());                 // ← NEW
        meter.setEanCode(request.eanCode());
        meter.setInstallationNumber(request.installationNumber());
        meter.setCustomerNumber(request.customerNumber());
        meter.setOwnerType(ownerType);
        meter.setOwnerId(ownerId);
        meter.setStartDate(request.startDate());
        meter.setEndDate(null);

        return meterMapper.toDTO(meterRepository.save(meter));
    }

    @Transactional
    public MeterDTO replaceMeter(String ownerType, Long ownerId, Long meterId, ReplaceMeterRequest request) {
        validateOwnerExists(ownerType, ownerId);

        Meter current = meterRepository.findByIdAndEndDateIsNull(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        validateStartDateNotFuture(request.newStartDate());

        if (request.newStartDate().isBefore(current.getStartDate())) {
            throw new MeterBusinessRuleException(
                    "Start date must be ≥ current meter start date (" + current.getStartDate() + ")");
        }

        validateConditionalFields(current.getType(), ownerType,
                request.newEanCode(), request.newInstallationNumber(), request.newCustomerNumber());

        current.setEndDate(request.newStartDate());
        meterRepository.save(current);

        Meter newMeter = new Meter();
        newMeter.setType(current.getType());
        newMeter.setMeterNumber(request.newMeterNumber());
        newMeter.setLabel(request.newLabel());           // ← NEW
        newMeter.setEanCode(request.newEanCode());
        newMeter.setInstallationNumber(request.newInstallationNumber());
        newMeter.setCustomerNumber(request.newCustomerNumber());
        newMeter.setOwnerType(ownerType);
        newMeter.setOwnerId(ownerId);
        newMeter.setStartDate(request.newStartDate());
        newMeter.setEndDate(null);

        return meterMapper.toDTO(meterRepository.save(newMeter));
    }
