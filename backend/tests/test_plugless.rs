use plugless::config::Config;
use plugless::doctor::run_doctor;
use plugless::protocol::*;

#[test]
fn test_protocol_serialization() {
    let msg = ClientMessage::MouseMove { dx: 12.5, dy: -8.0 };
    let json = serde_json::to_string(&msg).unwrap();
    assert!(json.contains("mouse_move"));
    assert!(json.contains("12.5"));

    let click = ClientMessage::MouseClick {
        button: MouseButton::Left,
        state: ButtonState::Click,
    };
    let json_click = serde_json::to_string(&click).unwrap();
    let deserialized: ClientMessage = serde_json::from_str(&json_click).unwrap();
    if let ClientMessage::MouseClick { button, state } = deserialized {
        assert_eq!(button, MouseButton::Left);
        assert_eq!(state, ButtonState::Click);
    } else {
        panic!("Wrong variant deserialized");
    }

    let auth = ServerMessage::AuthResponse {
        success: true,
        message: "Welcome".to_string(),
        audio_port: 8766,
    };
    let json_auth = serde_json::to_string(&auth).unwrap();
    assert!(json_auth.contains("auth_response"));
    assert!(json_auth.contains("8766"));
}

#[test]
fn test_config_trusted_devices() {
    let mut config = Config::default();
    assert!(config.trusted_devices.is_empty());

    config.add_or_update_device(
        "dev-123".to_string(),
        "Vivo y36".to_string(),
        "secret-token-abc".to_string(),
    );

    assert_eq!(config.trusted_devices.len(), 1);
    assert!(config.is_trusted("secret-token-abc").is_some());
    assert!(config.is_trusted("wrong-token").is_none());

    let removed = config.remove_device("Vivo y36");
    assert!(removed);
    assert_eq!(config.trusted_devices.len(), 0);
}

#[test]
fn test_doctor_runs() {
    let checks = run_doctor();
    assert!(!checks.is_empty());
    // Should have checked uinput, audio, ports, local ip, etc.
    assert!(checks.iter().any(|c| c.name.contains("Virtual Input")));
    assert!(checks.iter().any(|c| c.name.contains("Network Ports")));
}
