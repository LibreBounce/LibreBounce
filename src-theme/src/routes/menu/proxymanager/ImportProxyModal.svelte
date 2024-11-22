<!-- yes, I know, I pasted from the edit proxy modal, don't care. -->
<!-- probably will remove these comments later anyways... -->
<script lang="ts">
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
import Modal from "../common/modal/Modal.svelte";
    // import IconTextInput from "../common/setting/IconTextInput.svelte";
    // import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    // import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    // import {editProxy as editProxyRest} from "../../../integration/rest";

    export let visible: boolean;
</script>

<Modal title="Import Proxy" bind:visible={visible}>
    <IconTextButton
        title="From Clipboard" icon="icon-clipboard.svg" 
        on:click={() => alert("Not implemented yet")}/>
    <IconTextButton
        title="From File" icon="icon-file.svg"   
        on:click={() => alert("Not implemented yet")}/>
</Modal>
