package com.tornadic.item;

/**
 * Common-side hook for opening Tornadic screens. The client mod installs an
 * implementation; common item code stays free of client classes.
 */
public interface ScreenOpening {
	void openRadar();

	void openNotebook();
}
