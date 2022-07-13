param([string]$PackagePath = $null)

Write-Host "Path="$PackagePath

function PrintMessageAndExit($ErrorMessage, $ReturnCode)
{
    Write-Host $ErrorMessage
    exit $ReturnCode
}

function RegisterPackage
{
    $ManifestPath = Join-Path $PackagePath "AppxManifest.xml"

    Write-Host "Registering development package"
    
    $RegistrationSucceeded = $False
    try 
    {
        Add-AppxPackage -Register $ManifestPath
        $RegistrationSucceeded = $?
    }
    catch
    {
        $Error[0]
    }

    if ($RegistrationSucceeded)
    {
        PrintMessageAndExit "Appx Registration Succeeded" 0
    }
    else
    {
        PrintMessageAndExit "Appx Registration Succeeded" -1
    }    
}

RegisterPackage