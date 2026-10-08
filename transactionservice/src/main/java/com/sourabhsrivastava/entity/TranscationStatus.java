package com.sourabhsrivastava.entity;

/**
 * Transcation LifeCycle FLow:-
 * 
 * Pending-> Processing -> Completed (clean transcation)
 * pending- Processing- Pending_verfication(suspecious detected)
 *                             -> Completed(after verfied OTP)
 *                             -> Flagged (SAGA REFUND)
 *                      -> FAILED
 *                      ->FLAGGED
 * 
 */

public enum TranscationStatus {
	
	PENDING,
	PROCESSING,
	PENDING_VERIFICATION,
	COMPLTED,
	FAILED,
	FLAGGED

}
