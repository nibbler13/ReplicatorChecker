Set WshShell = CreateObject("WScript.Shell" ) 
WshShell.Run chr(34) & "run_check_replicator.bat" & Chr(34), 0 
Set WshShell = Nothing 