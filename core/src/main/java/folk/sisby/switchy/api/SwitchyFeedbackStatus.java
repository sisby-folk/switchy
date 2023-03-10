package folk.sisby.switchy.api;

/**
 * Defines the output of an API action, for providing feedback to players.
 * @see SwitchyFeedback
 */
public enum SwitchyFeedbackStatus {
	/**
	 * The action was performed.
	 */
	SUCCESS,
	/**
	 * The method must be run again to complete the action.
	 */
	CONFIRM,
	/**
	 * The specified action is not possible.
	 */
	INVALID,
	/**
	 * The specified action failed to complete.
	 */
	FAIL
}
