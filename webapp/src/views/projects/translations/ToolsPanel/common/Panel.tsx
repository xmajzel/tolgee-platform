import { Box, styled } from '@mui/material';
import { PanelConfig, PanelContentProps } from './types';
import { useState } from 'react';
import { KeyboardArrowDown, KeyboardArrowUp } from '@mui/icons-material';

const StyledContainer = styled(Box)`
  display: grid;
`;

const StyledHeader = styled(Box)`
  max-width: 100%;
  position: sticky;
  display: grid;
  grid-template-columns: auto auto auto 1fr;
  top: -1px;
  padding: 8px;
  gap: 8px;
  align-items: center;
  background: ${({ theme }) => theme.palette.background.default};
  z-index: 3;
  height: 39px;
`;

const StyledName = styled(Box)`
  flex-shrink: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 15px;
  text-transform: uppercase;
  font-weight: 500;
`;

const StyledBadge = styled(Box)`
  padding: 2px 4px;
  border-radius: 12px;
  font-size: 12px;
  height: 20px;
  min-width: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.emphasis[100]};
`;

const StyledContent = styled(Box)`
  min-height: 60px;
  padding-top: 0px;
  padding-bottom: 16px;
`;

const StyledToggle = styled(Box)`
  display: grid;
  cursor: pointer;
`;

type Props = PanelConfig & {
  data: Omit<PanelContentProps, 'setItemsCount'>;
  onToggle: () => void;
  open: boolean;
};

export const Panel = ({
  id,
  icon,
  name,
  component,
  data,
  itemsCountComponent,
  onToggle,
  open,
}: Props) => {
  const [itemsCount, setItemsCount] = useState<number | undefined>(undefined);
  const Component = component;
  const ItemsCountComponent = itemsCountComponent;

  return (
    <StyledContainer data-cy="translation-panel" data-cy-id={id}>
      <StyledHeader>
        {icon}
        <StyledName>{name}</StyledName>
        {typeof itemsCount === 'number' || ItemsCountComponent ? (
          <StyledBadge>
            {ItemsCountComponent ? (
              <ItemsCountComponent {...data} />
            ) : (
              itemsCount
            )}
          </StyledBadge>
        ) : (
          <div />
        )}
        <StyledToggle
          role="button"
          onClick={() => onToggle()}
          data-cy="translation-panel-toggle"
          data-cy-id={id}
        >
          {open ? (
            <KeyboardArrowUp fontSize="small" />
          ) : (
            <KeyboardArrowDown fontSize="small" />
          )}
        </StyledToggle>
      </StyledHeader>
      {open && (
        <StyledContent data-cy="translation-panel-content" data-cy-id={id}>
          <Component {...data} setItemsCount={setItemsCount} />
        </StyledContent>
      )}
    </StyledContainer>
  );
};
