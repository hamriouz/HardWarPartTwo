package hardwar.branch.prediction.judged.SAs;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC;
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // per set predication history table
    private final HashMode hashMode;

    public SAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public SAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;
        this.hashMode = hashMode;

        // Initialize the PSBHR with the given bhr and branch instruction size
        PSBHR = new RegisterBank(this.KSize, BHRSize);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PSPHT = new PerAddressPredictionHistoryTable(this.KSize, (int)Math.pow(2, BHRSize), SCSize);

        // Initialize the SC register
        SC = new SIPORegister("sc", SCSize, null);

    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        Bit[] address_PSBHR = getAddressLine(branchInstruction.getInstructionAddress());
        ShiftRegister shiftRegister_content_PSBHR = this.PSBHR.read(address_PSBHR);
        Bit[] history = shiftRegister_content_PSBHR.read();
        Bit[] address = getCacheEntry(address_PSBHR, history);
        Bit[] prediction = PSPHT.setDefault(address, getDefaultBlock());

        return BranchResult.of(prediction[0].getValue());

    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] address_PSBHR = getAddressLine(branchInstruction.getInstructionAddress());
        ShiftRegister shiftRegister_content_PSBHR = this.PSBHR.read(address_PSBHR);
        Bit[] history = shiftRegister_content_PSBHR.read();
        Bit[] address = getCacheEntry(address_PSBHR, history);
        Bit[] prediction = PSPHT.setDefault(address, getDefaultBlock());

        PSPHT.put(address, CombinationalLogic.count(prediction, BranchResult.isTaken(actual), CountMode.SATURATING));
        this.PSBHR.write(address_PSBHR, shiftRegister_content_PSBHR.read());
    }


    private Bit[] getAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return CombinationalLogic.hash(branchAddress, KSize, hashMode);
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // hash the branch address
        Bit[] hashKSize = CombinationalLogic.hash(branchAddress, KSize, hashMode);

        // Concatenate the Hash bits with the BHR bits
        Bit[] cacheEntry = new Bit[hashKSize.length + BHRValue.length];
        System.arraycopy(hashKSize, 0, cacheEntry, 0, hashKSize.length);
        System.arraycopy(BHRValue, 0, cacheEntry, hashKSize.length, BHRValue.length);

        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return null;
    }
}
