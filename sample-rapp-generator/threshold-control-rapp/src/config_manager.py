import json
import os
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


class ConfigManager:
    """Manages configuration loading and environment variable substitution"""
    
    def __init__(self, config_file: str = 'config.json'):
        self.config_file = config_file
        self.config = self._load_config()
        
    def _load_config(self) -> Dict[str, Any]:
        """Load configuration from file and apply environment variable substitutions"""
        try:
            with open(self.config_file, 'r') as f:
                config = json.load(f)
            
            # Apply environment variable substitutions
            config = self._substitute_env_vars(config)
            
            logger.info(f"Configuration loaded from {self.config_file}")
            return config
            
        except FileNotFoundError:
            logger.error(f"Configuration file {self.config_file} not found")
            raise
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in configuration file: {e}")
            raise
        except Exception as e:
            logger.error(f"Failed to load configuration: {e}")
            raise
    
    def _substitute_env_vars(self, obj: Any) -> Any:
        """Recursively substitute environment variables in configuration"""
        if isinstance(obj, dict):
            return {key: self._substitute_env_vars(value) for key, value in obj.items()}
        elif isinstance(obj, list):
            return [self._substitute_env_vars(item) for item in obj]
        elif isinstance(obj, str):
            # Check if string contains environment variable placeholder
            if obj.startswith('${') and obj.endswith('}'):
                env_var = obj[2:-1]  # Remove ${ and }
                # Support default values: ${VAR:default_value}
                if ':' in env_var:
                    var_name, default_value = env_var.split(':', 1)
                    return os.getenv(var_name, default_value)
                else:
                    return os.getenv(env_var, obj)  # Return original if env var not found
            return obj
        else:
            return obj
    
    def get_config(self) -> Dict[str, Any]:
        """Get the loaded configuration"""
        return self.config
    
    def get_section(self, section: str) -> Dict[str, Any]:
        """Get a specific configuration section"""
        return self.config.get(section, {})
    
    def get_value(self, path: str, default: Any = None) -> Any:
        """Get a configuration value using dot notation (e.g., 'SME.host')"""
        keys = path.split('.')
        value = self.config
        
        try:
            for key in keys:
                value = value[key]
            return value
        except (KeyError, TypeError):
            return default
    
    def update_config(self, updates: Dict[str, Any]) -> None:
        """Update configuration with new values"""
        self.config.update(updates)
        logger.info("Configuration updated")
    
    def reload_config(self) -> None:
        """Reload configuration from file"""
        self.config = self._load_config()
        logger.info("Configuration reloaded")